package models.basic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.HecticusModel;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.*;
import java.util.List;

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

    @OneToMany(mappedBy="timezone", cascade = CascadeType.ALL)
    private List<CountryHasTimezone> countries;

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

    public List<CountryHasTimezone> getCountries() {
        return countries;
    }

    public void setCountries(List<CountryHasTimezone> countries) {
        this.countries = countries;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_timezone", idTimezone);
        response.put("name", name);
        return response;
    }
}
