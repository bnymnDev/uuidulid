package io.github.bnymndev.uuidulid.hibernate;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/**
 * Registers {@link UlidJavaType} with every Hibernate {@code SessionFactory}.
 *
 * <p>Hibernate discovers this class through {@link java.util.ServiceLoader}
 * ({@code META-INF/services/org.hibernate.boot.model.TypeContributor}), so having
 * {@code uuidulid-hibernate} on the classpath is all that is needed.
 */
public class UlidTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        typeContributions.contributeJavaType(UlidJavaType.INSTANCE);
    }
}
