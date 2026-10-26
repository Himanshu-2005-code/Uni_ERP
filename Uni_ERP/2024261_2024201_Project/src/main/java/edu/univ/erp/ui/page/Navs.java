package edu.univ.erp.ui.page;

import edu.univ.erp.domain.Role;
import edu.univ.erp.ui.page.admin.AdminDashboard;
import edu.univ.erp.ui.page.admin.AuditPage;
import edu.univ.erp.ui.page.admin.BackupPage;
import edu.univ.erp.ui.page.admin.CoursesPage;
import edu.univ.erp.ui.page.admin.SectionsAdminPage;
import edu.univ.erp.ui.page.admin.SettingsPage;
import edu.univ.erp.ui.page.admin.UsersPage;
import edu.univ.erp.ui.page.common.NoticesPage;
import edu.univ.erp.ui.page.common.ProfilePage;
import edu.univ.erp.ui.page.instructor.AnalyticsPage;
import edu.univ.erp.ui.page.instructor.GradebookPage;
import edu.univ.erp.ui.page.instructor.InstructorDashboard;
import edu.univ.erp.ui.page.instructor.SectionsPage;
import edu.univ.erp.ui.page.student.CatalogPage;
import edu.univ.erp.ui.page.student.GradesPage;
import edu.univ.erp.ui.page.student.MyCoursesPage;
import edu.univ.erp.ui.page.student.StudentDashboard;
import edu.univ.erp.ui.page.student.TimetablePage;

import java.util.List;

/** The sidebar for each role. */
final class Navs {

    private Navs() {
    }

    static List<Nav> forRole(Role role) {
        return switch (role) {
            case STUDENT -> List.of(
                    new Nav("dashboard", "Dashboard", "grid", "Overview", StudentDashboard::new),
                    new Nav("catalog", "Course catalog", "book", "Academics", CatalogPage::new),
                    new Nav("courses", "My courses", "layers", "Academics", MyCoursesPage::new),
                    new Nav("grades", "Grades & transcript", "award", "Academics", GradesPage::new),
                    new Nav("timetable", "Timetable", "calendar", "Academics", TimetablePage::new),
                    new Nav("notices", "Notices", "megaphone", "Campus", NoticesPage::new),
                    new Nav("profile", "Profile & settings", "user", "Account", ProfilePage::new));
            case INSTRUCTOR -> List.of(
                    new Nav("dashboard", "Dashboard", "grid", "Overview", InstructorDashboard::new),
                    new Nav("sections", "My sections", "layers", "Teaching", SectionsPage::new),
                    new Nav("gradebook", "Gradebook", "clipboard", "Teaching", GradebookPage::new),
                    new Nav("analytics", "Class analytics", "bars", "Teaching", AnalyticsPage::new),
                    new Nav("notices", "Notices", "megaphone", "Campus", NoticesPage::new),
                    new Nav("profile", "Profile & settings", "user", "Account", ProfilePage::new));
            case ADMIN -> List.of(
                    new Nav("dashboard", "Dashboard", "grid", "Overview", AdminDashboard::new),
                    new Nav("users", "Users", "users", "Manage", UsersPage::new),
                    new Nav("courses", "Courses", "book", "Manage", CoursesPage::new),
                    new Nav("sections", "Sections & timetable", "calendar", "Manage", SectionsAdminPage::new),
                    new Nav("notices", "Notices", "megaphone", "Campus", NoticesPage::new),
                    new Nav("settings", "Academic settings", "sliders", "System", SettingsPage::new),
                    new Nav("backup", "Backup & restore", "database", "System", BackupPage::new),
                    new Nav("audit", "Audit log", "history", "System", AuditPage::new),
                    new Nav("profile", "Profile & settings", "user", "Account", ProfilePage::new));
        };
    }
}
