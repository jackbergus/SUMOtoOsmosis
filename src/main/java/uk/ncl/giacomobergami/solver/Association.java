package uk.ncl.giacomobergami.solver;

import java.util.Map;
import java.util.Objects;

public class Association {
    public final Vehicle veh;
    public final RSU     rsu;

    public Association(Vehicle veh, RSU rsu) {
        this.veh = veh;
        this.rsu = rsu;
    }

    public void populateMap(Map<Vehicle, RSU> map) {
        map.put(veh, rsu);
    }

    @Override
    public String toString() {
        return "Association{" +
                "veh=" + veh +
                ", rsu=" + rsu +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Association that = (Association) o;
        return Objects.equals(veh, that.veh) && Objects.equals(rsu, that.rsu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(veh, rsu);
    }
}
