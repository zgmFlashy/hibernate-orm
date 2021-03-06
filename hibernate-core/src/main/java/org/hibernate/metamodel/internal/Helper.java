/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class Helper {
	private static final Logger log = Logger.getLogger( Helper.class );

	private final SessionFactoryImplementor sessionFactory;

	public Helper(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public static interface AttributeSource {
		public Type findType(String attributeName);
	}

	public AttributeSource resolveAttributeSource(ManagedType managedType) {
		return resolveAttributeSource( sessionFactory, managedType );
	}

	public static AttributeSource resolveAttributeSource(SessionFactoryImplementor sessionFactory, ManagedType managedType) {
		if ( EmbeddableTypeImpl.class.isInstance( managedType ) ) {
			return new ComponentAttributeSource( ( (EmbeddableTypeImpl) managedType ).getHibernateType() );
		}
		else if ( IdentifiableType.class.isInstance( managedType ) ) {
			final String entityName = managedType.getJavaType().getName();
			log.debugf( "Attempting to resolve managed type as entity using %s", entityName );
			return new EntityPersisterAttributeSource( sessionFactory.getEntityPersister( entityName ) );
		}
		else {
			throw new IllegalArgumentException(
					String.format( "Unknown ManagedType implementation [%s]", managedType.getClass() )
			);
		}
	}

	public static class EntityPersisterAttributeSource implements AttributeSource {
		private final EntityPersister entityPersister;


		public EntityPersisterAttributeSource(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}

		@Override
		public Type findType(String attributeName) {
			return entityPersister.getPropertyType( attributeName );
		}
	}

	public static class ComponentAttributeSource implements AttributeSource {
		private final CompositeType compositeType;


		public ComponentAttributeSource(CompositeType compositeType) {
			this.compositeType = compositeType;
		}

		@Override
		public Type findType(String attributeName) {
			int i = 0;
			for ( String componentAttributeName : compositeType.getPropertyNames() ) {
				if ( attributeName.equals( componentAttributeName ) ) {
					return compositeType.getSubtypes()[i];
				}
				i++;
			}
			throw new IllegalArgumentException( "Could not find given attribute name [%s] on composite-type" );
		}
	}

	public Type resolveType(Attribute attribute) {
		return resolveType( sessionFactory, attribute );
	}

	public static Type resolveType(SessionFactoryImplementor sessionFactory, Attribute attribute) {
		return resolveAttributeSource( sessionFactory, attribute.getDeclaringType() ).findType( attribute.getName() );
	}
}
