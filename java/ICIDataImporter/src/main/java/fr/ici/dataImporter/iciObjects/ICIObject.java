package fr.ici.dataImporter.iciObjects;

public class ICIObject {
    String ID;

    public String getID() {
        return ID;
    }

    public String toString() {
        return ID;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object o) {
        return (o instanceof ICIObject) && (toString().equals(o.toString()));
    }
}
