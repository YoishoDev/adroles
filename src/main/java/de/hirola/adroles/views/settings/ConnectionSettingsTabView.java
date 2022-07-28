package de.hirola.adroles.views.settings;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.hirola.adroles.Global;
import de.hirola.adroles.data.entity.ActiveDirectory;
import de.hirola.adroles.service.IdentityService;
import de.hirola.adroles.views.MainLayout;
import de.hirola.adroles.views.NotificationPopUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import java.net.ConnectException;

@Route(value = "connection-setting", layout = MainLayout.class)
@PageTitle("Settings - Connection | AD-Roles")
@PermitAll
public class ConnectionSettingsTabView extends VerticalLayout implements ComponentEventListener<ClickEvent<Button>> {

    private final Logger logger = LoggerFactory.getLogger(ConnectionSettingsTabView.class);
    private final ActiveDirectory activeDirectory; //TODO: in v.0.1 only 1 DC is possible
    private final IdentityService service;
    private Button saveButton;
    private Button verifyButton;
    private final Binder<ActiveDirectory> activeDirectoryBinder = new BeanValidationBinder<>(ActiveDirectory.class);

    public ConnectionSettingsTabView(IdentityService service) {
        this.service = service;
        // load the objects from backend
        activeDirectory = service.getActiveDirectory();
        activeDirectoryBinder.setBean(activeDirectory);
        addClassName("dashboard-view");
        setDefaultHorizontalComponentAlignment(Alignment.START);
        add(SettingsTabBar.getTabs(1));
        addComponents();
    }

    private void addComponents() {
        TextField domainNameTextField = new TextField(getTranslation("domain.name"));
        domainNameTextField.setPlaceholder(getTranslation("domain.name.placeholder"));
        domainNameTextField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
        activeDirectoryBinder
                .forField(domainNameTextField)
                .withValidator(domainName -> domainName.length() > 0, getTranslation("error.input.all.empty"))
                .bind(ActiveDirectory::getDomainName, ActiveDirectory::setDomainName);
        add(domainNameTextField);

        TextField serverTextField = new TextField(getTranslation("domain.server.ip"));
        serverTextField.setPlaceholder(getTranslation("domain.server.ip.placeholder"));
        serverTextField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
        activeDirectoryBinder
                .forField(serverTextField)
                .withValidator(server -> server.length() > 0, getTranslation("error.input.all.empty"))
                .bind(ActiveDirectory::getIPAddress, ActiveDirectory::setIPAddress);
        add(serverTextField);

        NumberField serverPortTextField = new NumberField(getTranslation("domain.server.port"));
        serverPortTextField.setPlaceholder(getTranslation("domain.server.port.placeholder"));
        activeDirectoryBinder
                .forField(serverPortTextField)
                .withValidator(port -> port > 0, getTranslation("error.input.all.empty"))
                .bind(ActiveDirectory::getPort, ActiveDirectory::setPort);
        add(serverPortTextField);

        Checkbox secureConnectionCheckBox = new Checkbox(getTranslation("domain.secure"));
        activeDirectoryBinder
                .forField(secureConnectionCheckBox)
                .bind(ActiveDirectory::useSecureConnection, ActiveDirectory::setUseSecureConnection);
        add(secureConnectionCheckBox);

        TextField usernameTextField = new TextField(getTranslation("username"));
        usernameTextField.setPlaceholder("CN=AD-Roles,CN=Users,DC=example,DC=com");
        usernameTextField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
        activeDirectoryBinder
                .forField(usernameTextField)
                .withValidator(username -> username.length() > 0, getTranslation("error.input.all.empty"))
                .bind(ActiveDirectory::getConnectionUserName, ActiveDirectory::setConnectionUserName);
        add(usernameTextField);

        PasswordField passwordField = new PasswordField(getTranslation("password"));
        passwordField.setWidth(Global.Component.DEFAULT_TEXT_FIELD_WIDTH);
        activeDirectoryBinder
                .forField(passwordField)
                .withValidator(password -> password.length() > 0, getTranslation("error.input.all.empty"))
                .bind(ActiveDirectory::getEncryptedConnectionPassword, ActiveDirectory::setEncryptedConnectionPassword);
        add(passwordField);

        saveButton = new Button(getTranslation("save"));
        saveButton.setWidth(Global.Component.DEFAULT_BUTTON_WIDTH);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(this);
        add(saveButton);

        verifyButton = new Button(getTranslation("verify"));
        verifyButton.setWidth(Global.Component.DEFAULT_BUTTON_WIDTH);
        verifyButton.addClickListener(this);
        add(verifyButton);

    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (buttonClickEvent.getSource().equals(saveButton)) {
            try {
                // update form object from component values
                activeDirectoryBinder.writeBean(activeDirectory);
                service.saveActiveDirectory(activeDirectory);
                NotificationPopUp.show(NotificationPopUp.INFO, getTranslation("data.saved"));
            } catch (ValidationException exception) {
                NotificationPopUp.show(NotificationPopUp.ERROR, getTranslation("error.save"));
                logger.debug(exception.getLocalizedMessage());
            }
        }
        if (buttonClickEvent.getSource().equals(verifyButton)) {
            try {
                // update form object from component values
                activeDirectoryBinder.writeBean(activeDirectory);
                // test the connection
                service.verifyConnection(activeDirectory);
                NotificationPopUp.show(NotificationPopUp.INFO, getTranslation("domain.connected"));
            } catch (ValidationException | ConnectException exception) {
                NotificationPopUp.show(NotificationPopUp.ERROR, getTranslation("error.domain.connection"));
                logger.debug(exception.getMessage());
            }
        }
    }
}