package de.hirola.adroles.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import de.hirola.adroles.security.SecurityService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import de.hirola.adroles.views.adgroups.ADGroupsView;
import de.hirola.adroles.views.adusers.ADUsersView;
import de.hirola.adroles.views.organizations.OrganizationsView;
import de.hirola.adroles.views.persons.PersonListView;
import de.hirola.adroles.views.roles.RolesView;
import de.hirola.adroles.views.settings.SettingsView;

public class MainLayout extends AppLayout {
    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1(getTranslation("app.name"));
        logo.addClassNames("text-l", "m-m");

        Button logout = new Button(getTranslation("logout"), e -> securityService.logout());

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);

    }

    private void createDrawer() {
        Tabs tabs = getTabs();
        addToDrawer(tabs);
    }

    private Tabs getTabs() {
        Tabs tabs = new Tabs();
        tabs.add(
                createTab(VaadinIcon.DASHBOARD, DashboardView.class, getTranslation("dashboard")),
                createTab(VaadinIcon.USERS, PersonListView.class, getTranslation("persons")),
                createTab(VaadinIcon.OFFICE, OrganizationsView.class, getTranslation("organizations")),
                createTab(VaadinIcon.RECORDS, RolesView.class, getTranslation("roles")),
                createTab(VaadinIcon.USER_STAR, ADUsersView.class, getTranslation("aduser")),
                createTab(VaadinIcon.GROUP, ADGroupsView.class, getTranslation("adgroup")),
                createTab(VaadinIcon.CONTROLLER, SettingsView.class, getTranslation("settings"))
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        return tabs;
    }

    private Tab createTab(VaadinIcon viewIcon, Class<? extends Component> viewClass, String menuText) {
        Icon icon = viewIcon.create();
        icon.getStyle()
                .set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(menuText));
        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }
}
