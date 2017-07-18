package model;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.util.UUID;

@MappedSuperclass
public abstract class PersistableObject {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID databaseId = UUID.randomUUID();

    @Version
    Long databaseVersion;


    PersistableObject() {

    }

    public UUID getDatabaseId() {
        return databaseId;
    }

    public Long getDatabaseVersion() {
        return databaseVersion;
    }

    @Override
    public String toString() {
        String className = this.getClass().getName();
        return className + (isNew() ? " (transient)" : "");
    }

    public boolean isNew() {
        return databaseVersion == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || !(o instanceof PersistableObject))
            return false;

        if (databaseId == null)
            return false;

        PersistableObject other = (PersistableObject) o;
        return databaseId.equals(other.databaseId);
    }

    @Override
    public int hashCode() {
        if (databaseId != null) {
            return databaseId.hashCode();
        } else {
            return super.hashCode();
        }
    }
}