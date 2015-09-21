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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by plessmann on 14/07/15.
 */
@Entity
@Table(name="timezones")
public class Timezone extends HecticusModel {

    @Id
    private Integer idTimezone;
    @Constraints.Required
    private String name;

//    @OneToMany(mappedBy="timezone", cascade = CascadeType.ALL)
//    private List<CountryHasTimezone> countries;

    public static Model.Finder<Integer, Timezone> finder = new Model.Finder<>(Integer.class, Timezone.class);

    public Integer getIdTimezone() {
        return idTimezone;
    }

    public void setIdTimezone(Integer idTimezone) {
        this.idTimezone = idTimezone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public List<CountryHasTimezone> getCountries() {
//        return countries;
//    }

//    public void setCountries(List<CountryHasTimezone> countries) {
//        this.countries = countries;
//    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_timezone", idTimezone);
        response.put("name", name);
        return response;
    }

    public static scala.collection.immutable.List<Tuple2<String, String>> toSeq() {
        List<Timezone> timezones = Timezone.finder.all();
        ArrayList<Tuple2<String, String>> proxy = new ArrayList<>();
        for(Timezone timezone : timezones) {
            Tuple2<String, String> t = new Tuple2<>(timezone.getIdTimezone().toString(), timezone.getName());
            proxy.add(t);
        }
        Buffer<Tuple2<String, String>> timezonesBuffer = JavaConversions.asScalaBuffer(proxy);
        scala.collection.immutable.List<Tuple2<String, String>> timezonesList = timezonesBuffer.toList();
        return timezonesList;
    }

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        List<Timezone> timezones = finder.all();
        for(Timezone l: timezones) {
            options.put(l.getIdTimezone().toString(), l.getName());
        }
        return options;
    }

    public static Page<Timezone> page(int page, int pageSize, String sortBy, String order, String filter) {
        return finder.where().orderBy(sortBy + " " + order).findPagingList(pageSize).getPage(page);
    }



    public static Timezone getByID(int id){
        return finder.byId(id);
    }
}
