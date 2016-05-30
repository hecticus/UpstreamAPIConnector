package models.basic;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import models.HecticusModel;
import models.clients.ClientHasDevices;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

import javax.persistence.*;
import java.util.*;

/**
 * Created by plesse on 9/30/14.
 */
@Entity
@Table(name="countries")
public class Country extends HecticusModel {

    @Id
    private Integer idCountry;
    @Constraints.Required
    private String name;
    @Constraints.Required
    private String shortName;
    private Boolean active;

    @OneToOne
    @JoinColumn(name = "id_language")
    private Language language;

    @OneToMany(mappedBy="country", cascade = CascadeType.ALL)
    private List<CountryHasTimezone> timezones;

    public static Model.Finder<Integer, Country> finder = new Model.Finder<>(Integer.class, Country.class);

    public Integer getIdCountry() {
        return idCountry;
    }

    public void setIdCountry(int idCountry) {
        this.idCountry = idCountry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<CountryHasTimezone> getTimezones() {
        return timezones;
    }

    public void setTimezones(List<CountryHasTimezone> timezones) {
        this.timezones = timezones;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_country", idCountry);
        response.put("name", name);
        response.put("short_name", shortName);
        response.put("active", active);
        response.put("language", language.toJson());
        if(timezones != null && !timezones.isEmpty()){
            ArrayList<ObjectNode> timezonesNodes = new ArrayList<>();
            for(CountryHasTimezone countryHasTimezone : timezones){
                timezonesNodes.add(countryHasTimezone.getTimezone().toJson());
            }
            response.put("timezones", Json.toJson(timezonesNodes));
        }
        return response;
    }

    public ObjectNode toJsonWithNoTimezones() {
        ObjectNode response = Json.newObject();
        response.put("id_country", idCountry);
        response.put("name", name);
        response.put("short_name", shortName);
        response.put("active", active);
        response.put("language", language.toJson());
        return response;
    }

    public ObjectNode toJsonSimple() {
        ObjectNode response = Json.newObject();
        response.put("id_country", idCountry);
        response.put("short_name", shortName);
        return response;
    }

    public Timezone getActiveTimezone(){
        Timezone timezone = null;
        try {
            CountryHasTimezone relation = Iterables.find(timezones, new Predicate<CountryHasTimezone>() {
                public boolean apply(CountryHasTimezone obj) {
                    return obj.getActive();
                }
            });
            timezone = relation.getTimezone();
        } catch (NoSuchElementException ex){
            timezone = null;
        }
        return timezone;
    }

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        List<Country> countries = Country.finder.all();
        for(Country c: countries) {
            options.put(c.getIdCountry().toString(), c.getName());
        }
        return options;
    }

    public static scala.collection.immutable.List<Tuple2<String, String>> toSeq() {
        List<Country> countries = Country.finder.all();
        ArrayList<Tuple2<String, String>> proxy = new ArrayList<>();
        for(Country country : countries) {
            Tuple2<String, String> t = new Tuple2<>(country.getIdCountry().toString(), country.getName());
            proxy.add(t);
        }
        Buffer<Tuple2<String, String>> countryBuffer = JavaConversions.asScalaBuffer(proxy);
        scala.collection.immutable.List<Tuple2<String, String>> countryList = countryBuffer.toList();
        return countryList;
    }

    public static Page<Country> page(int page, int pageSize, String sortBy, String order, String filter) {
        return finder.where().orderBy(sortBy + " " + order).findPagingList(pageSize).getPage(page);
    }

    public static Country getByID(int id){
        return finder.byId(id);
    }

}
