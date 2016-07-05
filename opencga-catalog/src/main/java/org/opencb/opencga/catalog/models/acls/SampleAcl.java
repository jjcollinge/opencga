package org.opencb.opencga.catalog.models.acls;

import org.opencb.commons.datastore.core.ObjectMap;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by pfurio on 11/05/16.
 */
public class SampleAcl extends ParentAcl<SampleAcl.SamplePermissions> {

    public enum SamplePermissions {
        VIEW,
        UPDATE,
        DELETE,
        SHARE,
        CREATE_ANNOTATIONS,
        VIEW_ANNOTATIONS,
        UPDATE_ANNOTATIONS,
        DELETE_ANNOTATIONS
    }

    public SampleAcl() {
        this("", Collections.emptyList());
    }

    public SampleAcl(String member, EnumSet<SamplePermissions> permissions) {
        super(member, permissions);
    }

    public SampleAcl(String member, ObjectMap permissions) {
        super(member, EnumSet.noneOf(SamplePermissions.class));

        EnumSet<SamplePermissions> aux = EnumSet.allOf(SamplePermissions.class);
        for (SamplePermissions permission : aux) {
            if (permissions.containsKey(permission.name()) && permissions.getBoolean(permission.name())) {
                this.permissions.add(permission);
            }
        }
    }

    public SampleAcl(String member, List<String> permissions) {
        super(member, EnumSet.noneOf(SamplePermissions.class));
        if (permissions.size() > 0) {
            this.permissions.addAll(permissions.stream().map(SamplePermissions::valueOf).collect(Collectors.toList()));
        }
    }

}
