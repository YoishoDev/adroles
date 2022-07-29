package de.hirola.adroles.views.roles;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import de.hirola.adroles.Global;
import de.hirola.adroles.data.entity.Person;
import de.hirola.adroles.data.entity.Role;
import de.hirola.adroles.service.IdentityService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ResourceAssignADGroupForm extends VerticalLayout {
  private final IdentityService identityService;
  private Role role;
  private final Set<Person> selectedPersons = new LinkedHashSet<>();
  private TextField roleTexField, searchField;
  private Button assignFromPersonsButton;
  private final Grid<Person> grid = new Grid<>(Person.class, false);

  private GridListDataView<Person> dataView;
  public ResourceAssignADGroupForm(IdentityService identityService) {
    this.identityService = identityService;
    addClassName("role-assign-adgroup-form");
    addComponents();
  }

  private void addComponents() {

    roleTexField = new TextField(getTranslation("org"));
    roleTexField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
    roleTexField.setReadOnly(true);
    add(roleTexField);

    searchField = new TextField();
    searchField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
    searchField.setPlaceholder(getTranslation("search"));
    searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    searchField.addValueChangeListener(event -> {
      if (dataView != null) {
        dataView.refreshAll();
      }
    });
    add(searchField);

    assignFromPersonsButton = new Button(getTranslation("org.assignFromPersons"), new Icon(VaadinIcon.DOWNLOAD));
    assignFromPersonsButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    assignFromPersonsButton.setWidth(Global.Component.DEFAULT_BUTTON_WIDTH);
    assignFromPersonsButton.addClickListener(click -> assignFromPersons());
    add(assignFromPersonsButton);

    grid.setSizeFull();
    grid.addClassNames("person-grid");
    grid.addColumn(person -> selectedPersons.contains(person) ? getTranslation("assigned") : getTranslation("notAssigned"), "status")
            .setHeader(getTranslation("status"))
            .setKey(Global.Component.FOOTER_COLUMN_KEY)
            .setSortOrderProvider(direction -> Stream.of(new QuerySortOrder("status", direction)))
            .setComparator((person1, person2) -> {
              if ((selectedPersons.contains(person1) && selectedPersons.contains(person2)) ||
                      (!selectedPersons.contains(person1) && !selectedPersons.contains(person2)) ) {
                return 0;
              }
              if (selectedPersons.contains(person1) && !selectedPersons.contains(person2)) {
                return 1;
              }
              return -1;
            });
    grid.addColumn(Person::getLastName).setHeader(getTranslation("lastname"))
            .setSortable(true);
    grid.addColumn(Person::getFirstName).setHeader(getTranslation("firstname"))
            .setSortable(true);
    grid.addColumn(Person::getCentralAccountName)
            .setHeader(getTranslation("centralAccountName"))
            .setSortable(true);
    grid.addColumn(Person::getDepartmentName).setHeader(getTranslation("department"))
            .setSortable(true);
    grid.getColumns().forEach(col -> col.setAutoWidth(true));
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    grid.addSelectionListener(selection -> {
      selectedPersons.clear();
      selectedPersons.addAll(selection.getAllSelectedItems());
    });

    add(grid);

    Button saveButton = new Button(getTranslation("save"));
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickShortcut(Key.ENTER);
    saveButton.addClickListener(event -> validateAndSave());

    Button closeButton = new Button(getTranslation("cancel"));
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    closeButton.addClickShortcut(Key.ESCAPE);
    closeButton.addClickListener(event -> this.setVisible(false));
    closeButton.addClickListener(event -> fireEvent(new CloseEvent(this)));

    HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, closeButton);
    buttonsLayout.setPadding(true);

    add(buttonsLayout);
  }

  public void setData(Role role, List<Person> persons) {
    this.role = role;
    if (role != null && persons != null) {
      // build org unit info string
      StringBuilder orgUnitInfos = new StringBuilder(role.getName());
      if (role.getDescription().length() > 0) {
        orgUnitInfos.append(" (");
        orgUnitInfos.append(role.getDescription());
        orgUnitInfos.append(")");
      }
      roleTexField.setValue(orgUnitInfos.toString());

      // disable the assign button, if no persons available
      assignFromPersonsButton.setEnabled(persons.size() > 0);

      // you can filter the grid
      dataView = grid.setItems(persons);
      dataView.addFilter(person -> {
        String searchTerm = searchField.getValue().trim();
        if (searchTerm.isEmpty()) {
          return true;
        }
        boolean matchesLastName = matchesTerm(person.getLastName(), searchTerm);
        boolean matchesFirstName = matchesTerm(person.getFirstName(), searchTerm);
        boolean matchesCentralAccountName = matchesTerm(person.getCentralAccountName(), searchTerm);
        boolean matchesDepartmentName = matchesTerm(person.getDepartmentName(), searchTerm);

        return matchesLastName || matchesFirstName || matchesCentralAccountName || matchesDepartmentName;
      });

      // show first assigned persons
      dataView.setSortOrder((ValueProvider<Person, String>) person -> {
        if (person == null) {
          return "";
        }
        if (selectedPersons.contains(person)) {
          return getTranslation("assigned");
        }
        return getTranslation("notAssigned");
      }, SortDirection.DESCENDING);

      // add assigned roles to selected list
      selectedPersons.clear();
      selectedPersons.addAll(role.getPersons());
      grid.asMultiSelect().select(selectedPersons);
      grid.getColumnByKey(Global.Component.FOOTER_COLUMN_KEY)
              .setFooter(String.format(getTranslation("persons.assigned") + ": %s", selectedPersons.size()));
    }
  }

  private boolean matchesTerm(String value, String searchTerm) {
    return value.toLowerCase().contains(searchTerm.toLowerCase());
  }

  private void assignFromPersons() {
    selectedPersons.addAll(identityService.findAllPersonsWithDepartmentName(role.getName()));
    grid.getColumnByKey(Global.Component.FOOTER_COLUMN_KEY)
            .setFooter(String.format(getTranslation("persons.assigned") + ": %s", selectedPersons.size()));
    grid.asMultiSelect().select(selectedPersons);
  }
  private void validateAndSave() {
    if (role.getPersons().isEmpty()) {
      role.setPersons(selectedPersons);
    }  else {
      role.removeAllPersons();
      role.setPersons(selectedPersons);
    }
    fireEvent(new SaveEvent(this, role));
  }

  // Events
  public static abstract class RoleAssignPersonFormEvent extends ComponentEvent<ResourceAssignADGroupForm> {
    private final Role role;

    protected RoleAssignPersonFormEvent(ResourceAssignADGroupForm source, Role role) {
      super(source, false);
      this.role = role;
    }

    public Role getOrgUnit() {
      return role;
    }
  }

  public static class SaveEvent extends RoleAssignPersonFormEvent {
    SaveEvent(ResourceAssignADGroupForm source, Role role) {
      super(source, role);
    }
  }

  public static class CloseEvent extends RoleAssignPersonFormEvent {
    CloseEvent(ResourceAssignADGroupForm source) {
      super(source, null);
    }
  }

  public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
    return getEventBus().addListener(eventType, listener);
  }
}