package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.Config;
import models.basic.Timezone;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import views.html.timezones.edit;
import java.io.File;

import static play.data.Form.form;

/**
 * Created by plessmann on 30/07/15.
 */
public class TimezonesView extends HecticusController {

    final static Form<Timezone> TimezoneViewForm = form(Timezone.class);
    public static Result GO_HOME = redirect(routes.TimezonesView.list(0, "name", "asc", ""));

    @Restrict(@Group(Application.USER_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result blank() {
        return ok(views.html.timezones.form.render(TimezoneViewForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.timezones.list.render(Timezone.page(page, 10, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result edit(Integer id) {
        Timezone timezone = Timezone.getByID(id);
        Form<Timezone> filledForm = TimezoneViewForm.fill(timezone);
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result update(Integer id) {
        Form<Timezone> filledForm = TimezoneViewForm.bindFromRequest();
        try {
            if (filledForm.hasErrors()) {
                return badRequest(edit.render(id, filledForm));
            }
            Timezone timezone = filledForm.get();
            timezone.update(id);
            flash("success", Messages.get("timezones.java.updated", timezone.getName()));
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
            Timezone timezone = Timezone.getByID(Integer.parseInt(aids[i]));
            timezone.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result lsort() {
        return ok(views.html.timezones.list.render(Timezone.page(0, 0, "name", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result delete(Integer id) {
        Timezone timezone = Timezone.getByID(id);
        timezone.delete();
        flash("success", Messages.get("timezones.java.deleted", timezone.getName()));
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result submit(){
        Form<Timezone> filledForm = TimezoneViewForm.bindFromRequest();
        try {
            if (filledForm.hasErrors()) {
                return badRequest(views.html.timezones.form.render(filledForm));
            }
            Timezone gfilledForm = filledForm.get();
            gfilledForm.save();
            flash("success", Messages.get("timezones.java.created", gfilledForm.getName()));
            return GO_HOME;
        } catch (Exception e){
            return badRequest(views.html.timezones.form.render(filledForm));
        }
    }
}
