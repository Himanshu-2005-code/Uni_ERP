package edu.univ.erp.service;

import edu.univ.erp.data.AuditDAO;
import edu.univ.erp.data.UserDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.domain.User;
import org.mindrot.jbcrypt.BCrypt;

/** Sign-in, sign-out and password changes. */
public final class AuthService {

    public static final int MIN_PASSWORD = 8;

    private AuthService() {
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public static void checkPolicy(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new ServiceException("Password must be at least " + MIN_PASSWORD + " characters.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ServiceException("Password must contain both letters and numbers.");
        }
    }

    /**
     * Verifies credentials. Wrong passwords count towards the lockout threshold;
     * the same message is used for unknown users so usernames can't be probed.
     */
    public static User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ServiceException("Enter your username and password.");
        }
        int threshold = SettingsService.get().refresh().lockoutThreshold();
        return Database.tx(c -> {
            var found = UserDAO.credentials(c, username.trim());
            if (found.isEmpty()) {
                throw new ServiceException("Incorrect username or password.");
            }
            User u = found.get().user();
            if (u.isLocked()) {
                throw new ServiceException("This account is locked. Ask an administrator to unlock it.");
            }
            boolean ok;
            try {
                ok = BCrypt.checkpw(password, found.get().passwordHash());
            } catch (IllegalArgumentException badHash) {
                ok = false;
            }
            if (!ok) {
                int attempts = UserDAO.recordFailure(c, u.id(), threshold);
                AuditDAO.log(c, u.id(), u.username(), "LOGIN_FAILED", "Attempt " + attempts);
                c.commit(); // keep the failure count even though we throw below
                int left = threshold - attempts;
                if (left <= 0) {
                    throw new ServiceException("Too many failed attempts. The account is now locked.");
                }
                throw new ServiceException("Incorrect username or password. " + left
                        + (left == 1 ? " attempt" : " attempts") + " left before the account locks.");
            }
            UserDAO.recordSuccess(c, u.id());
            AuditDAO.log(c, u.id(), u.username(), "LOGIN", null);
            User fresh = UserDAO.find(c, u.id()).orElse(u);
            Session.set(fresh);
            return fresh;
        });
    }

    public static void logout() {
        if (Session.isSignedIn()) {
            User u = Session.user();
            Database.txRun(c -> AuditDAO.log(c, u.id(), u.username(), "LOGOUT", null));
        }
        Session.clear();
    }

    public static void changePassword(String current, String next) {
        User u = Session.user();
        checkPolicy(next);
        if (next.equals(current)) throw new ServiceException("The new password must be different.");
        Database.txRun(c -> {
            String hash = UserDAO.passwordHash(c, u.id()).orElseThrow();
            if (!BCrypt.checkpw(current, hash)) throw new ServiceException("Your current password is incorrect.");
            UserDAO.setPassword(c, u.id(), hash(next));
            Audit.log(c, "PASSWORD_CHANGED", null);
        });
    }

    /** Updates the signed-in user's own name and email. */
    public static User updateContact(String fullName, String email) {
        User u = Session.user();
        Validate.email(email);
        return Database.tx(c -> {
            UserDAO.updateContact(c, u.id(), Validate.trimToNull(fullName), Validate.trimToNull(email));
            Audit.log(c, "PROFILE_UPDATED", null);
            User fresh = UserDAO.find(c, u.id()).orElseThrow();
            Session.set(fresh);
            return fresh;
        });
    }
}
