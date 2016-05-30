package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.clients.Device;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Result;
import views.html.devices.edit;

import static play.data.Form.form;

/**
 * Created by plessmann on 30/07/15.
 */
public class DevicesView extends UpstreamController {

    final static Form<Device> DeviceViewForm = form(Device.class);
    public static Result GO_HOME = redirect(routes.DevicesView.list(0, "name", "asc", ""));

    @Restrict(@Group(Application.USER_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result blank() {
        return ok(views.html.devices.form.render(DeviceViewForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.devices.list.render(Device.page(page, 10, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result edit(Integer id) {
        Device device = Device.getByID(id);
        Form<Device> filledForm = DeviceViewForm.fill(device);
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result update(Integer id) {
        Form<Device> filledForm = DeviceViewForm.bindFromRequest();
        try {
            if (filledForm.hasErrors()) {
                return badRequest(edit.render(id, filledForm));
            }
            Device device = filledForm.get();
            device.update(id);
            flash("success", Messages.get("devices.java.updated", device.getName()));
            return GO_HOME;
        } catch (Exception e){
            e.printStackTrace();
            return badRequest(edit.render(id, filledForm));
        }
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result sort(String ids) {
        String[] aids = ids.split(",");

        for (int i=0; i<aids.length; i++) {
            Device device = Device.getByID(Integer.parseInt(aids[i]));
            device.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result lsort() {
        return ok(views.html.devices.list.render(Device.page(0, 0, "name", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result delete(Integer id) {
        Device device = Device.getByID(id);
        device.delete();
        flash("success", Messages.get("devices.java.deleted", device.getName()));
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result submit(){
        Form<Device> filledForm = DeviceViewForm.bindFromRequest();
        try {
            if (filledForm.hasErrors()) {
                return badRequest(views.html.devices.form.render(filledForm));
            }
            Device gfilledForm = filledForm.get();
            gfilledForm.save();
            flash("success", Messages.get("devices.java.created", gfilledForm.getName()));
            return GO_HOME;
        } catch (Exception e){
            return badRequest(views.html.devices.form.render(filledForm));
        }
    }
}
