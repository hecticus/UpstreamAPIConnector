package models.basic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.HecticusModel;
import models.clients.Client;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.*;

/**
 * Created by plessmann on 14/07/15.
 */
@Entity
@Table(name="country_has_timezone")
public class CountryHasTimezone extends HecticusModel {

    @Id
    private Integer idCountryHasTimezone;

    @ManyToOne
    @JoinColumn(name = "id_country")
    private Country country;

    @ManyToOne
    @JoinColumn(name = "id_timezone")
    private Timezone timezone;

    private Boolean active;

    public static Model.Finder<Integer, CountryHasTimezone> finder = new Model.Finder<>(Integer.class, CountryHasTimezone.class);

    public Integer getIdCountryHasTimezone() {
        return idCountryHasTimezone;
    }

    public void setIdCountryHasTimezone(Integer idCountryHasTimezone) {
        this.idCountryHasTimezone = idCountryHasTimezone;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Timezone getTimezone() {
        return timezone;
    }

    public void setTimezone(Timezone timezone) {
        this.timezone = timezone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_country_has_timezone", idCountryHasTimezone);
        response.put("country", country.toJsonWithNoTimezones());
        response.put("timezone", timezone.toJson());
        return response;
    }
}
