package models.basic;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.HecticusModel;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by plesse on 9/30/14.
 */

@Entity
@Table(name="languages")
public class Language extends HecticusModel {

    @Id
    private Integer idLanguage;
    @Constraints.Required
    private String name;
    @Constraints.Required
    private String shortName;
    @Constraints.Required
    private Integer active;

    @Constraints.Required
    private String appLocalizationFile;

    public static Model.Finder<Integer, Language> finder = new Model.Finder<Integer, Language>(Integer.class, Language.class);

    public Integer getIdLanguage() {
        return idLanguage;
    }

    public void setIdLanguage(int idLanguage) {
        this.idLanguage = idLanguage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public void setIdLanguage(Integer idLanguage) {
        this.idLanguage = idLanguage;
    }

    public String getAppLocalizationFile() {
        return appLocalizationFile;
    }

    public void setAppLocalizationFile(String appLocalizationFile) {
        this.appLocalizationFile = appLocalizationFile;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_language", idLanguage);
        response.put("name", name);
        response.put("short_name", shortName);
        response.put("active", active);
        return response;
    }

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        List<Language> languages = finder.where().eq("active", 1).findList();
        for(Language l: languages) {
            options.put(l.getIdLanguage().toString(), l.getName());
        }
        return options;
    }

    public static scala.collection.immutable.List<Tuple2<String, String>> toSeq() {
        List<Language> languages = finder.where().eq("active", 1).findList();
        ArrayList<Tuple2<String, String>> proxy = new ArrayList<>();
        for(Language l : languages) {
            Tuple2<String, String> t = new Tuple2<>(l.getIdLanguage().toString(), l.getName());
            proxy.add(t);
        }
        Buffer<Tuple2<String, String>> languageBuffer = JavaConversions.asScalaBuffer(proxy);
        scala.collection.immutable.List<Tuple2<String, String>> languageList = languageBuffer.toList();
        return languageList;
    }

    public static Page<Language> page(int page, int pageSize, String sortBy, String order, String filter) {
        return finder.where().orderBy(sortBy + " " + order).findPagingList(pageSize).getPage(page);
    }

    public static List<Language> getActiveLanguages(){
        return finder.where().eq("active", 1).findList();
    }

    public static Language getLanguageByShortName(String lang){
        return finder.where().eq("active", 1).eq("shortName", lang).findUnique();
    }

    public static Language getByID(int id){
        return finder.byId(id);
    }

}
