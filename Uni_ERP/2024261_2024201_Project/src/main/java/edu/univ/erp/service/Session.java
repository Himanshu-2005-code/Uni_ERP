package edu.univ.erp.service;

import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;

/** The signed-in user for this desktop session. */
public final class Session {

    private static volatile User current;

    private Session() {
    }

    public static User user() {
        if (current == null) throw new ServiceException("You are not signed in.");
        return current;
    }

    public static boolean isSignedIn() {
        return current != null;
    }

    static void set(User user) {
        current = user;
    }

    public static void clear() {
        current = null;
    }

    static void require(Role role) {
        if (user().role() != role) {
            throw new ServiceException("This action is only available to " + role.label().toLowerCase() + "s.");
        }
    }

    /** Blocks changes by non-admins while maintenance mode is on. */
    static void requireWritable() {
        if (user().role() != Role.ADMIN && SettingsService.get().maintenance()) {
            throw new ServiceException("The system is in maintenance mode, so changes are paused. Try again later.");
        }
    }
}
