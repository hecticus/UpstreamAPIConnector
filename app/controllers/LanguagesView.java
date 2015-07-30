package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.Config;
import models.basic.Language;
import org.apache.commons.io.FileUtils;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import views.html.languages.edit;

import java.io.File;

import static play.data.Form.form;

/**
 * Created by plesse on 10/24/14.
 */
public class LanguagesView extends HecticusController {

    final static Form<Language> LanguageViewForm = form(Language.class);
    public static Result GO_HOME = redirect(routes.LanguagesView.list(0, "name", "asc", ""));

    @Restrict(@Group(Application.USER_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result blank() {
        return ok(views.html.languages.form.render(LanguageViewForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.languages.list.render(Language.page(page, 10, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result edit(Integer id) {
        Language language = Language.getByID(id);
        Form<Language> filledForm = LanguageViewForm.fill(language);
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result update(Integer id) {
        Form<Language> filledForm = LanguageViewForm.bindFromRequest();
        try {
            Language language = Language.getByID(id);
            System.out.println(filledForm.toString());
            if (filledForm.hasErrors()) {

                return badRequest(edit.render(id, filledForm));
            }
            if (filledForm.data().containsKey("appLocalizationFile")) {
                String appLocalizationFile = filledForm.data().get("appLocalizationFile");
                if(appLocalizationFile != null && !appLocalizationFile.isEmpty()) {
                    if (!language.getAppLocalizationFile().equalsIgnoreCase(appLocalizationFile)) {
                        Http.MultipartFormData body = request().body().asMultipartFormData();
                        Http.MultipartFormData.FilePart picture = body.getFile("appLocalizationFile");
                        File localization = picture.getFile();
                        File folder = new File(Config.getString("locales-path"));
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }
                        File dest = new File(Config.getString("locales-path") + "/" + appLocalizationFile);
                        FileUtils.copyFile(localization, dest);
                    }
                }
            }
            Language gfilledForm = filledForm.get();
            gfilledForm.setActive(filledForm.data().containsKey("active"));
            gfilledForm.update(id);
            flash("success", Messages.get("languages.java.updated", gfilledForm.getName()));
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
            Language language = Language.getByID(Integer.parseInt(aids[i]));
            language.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result lsort() {
        return ok(views.html.languages.list.render(Language.page(0, 0, "name", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result delete(Integer id) {
        Language language = Language.getByID(id);
        language.delete();
        flash("success", Messages.get("languages.java.deleted", language.getName()));
        return GO_HOME;
    }

    @Restrict(@Group(Application.USER_ROLE))
    public static Result submit(){
        Form<Language> filledForm = LanguageViewForm.bindFromRequest();
        try {

            if (filledForm.hasErrors()) {
                return badRequest(views.html.languages.form.render(filledForm));
            }
            Language gfilledForm = filledForm.get();
            if (filledForm.data().containsKey("appLocalizationFile")) {
                String appLocalizationFile = filledForm.data().get("appLocalizationFile");
                if(appLocalizationFile != null && !appLocalizationFile.isEmpty()) {
                    Http.MultipartFormData body = request().body().asMultipartFormData();
                    Http.MultipartFormData.FilePart localizationPart = body.getFile("appLocalizationFile");
                    File localization = localizationPart.getFile();
                    File folder = new File(Config.getString("locales-path"));
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    File dest = new File(Config.getString("locales-path") + "/" + localization.getName());
                    FileUtils.copyFile(localization, dest);
                }
            }
            gfilledForm.setActive(filledForm.data().containsKey("active"));
            gfilledForm.save();
            flash("success", Messages.get("languages.java.created", gfilledForm.getName()));
            return GO_HOME;
        } catch (Exception e){
            return badRequest(views.html.languages.form.render(filledForm));
        }
    }
}
