package com.airepublic.microverse.discovery.configured;
/**
   Copyright 2015 Torsten Oltmanns, ai-republic GmbH, Germany

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class JPAPersistenceTest implements Serializable {
	private static final long serialVersionUID = 1L;
	private EntityManager entityManager;


	@Before
	public void init() {
		final Map<String, String> properties = new HashMap<>();
		// properties.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
		// properties.put("javax.persistence.jdbc.url", "jdbc:derby:memory:sampleDB;create=true");

		properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
		properties.put("javax.persistence.jdbc.url", "jdbc:oracle:thin:@//localhost:1521/XE");
		properties.put("javax.persistence.jdbc.user", "MICROVERSE");
		properties.put("javax.persistence.jdbc.password", "MICROVERSE_PWD");

		properties.put("javax.persistence.schema-generation.database.action", "create");

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("microverse-pu", properties);
		entityManager = emf.createEntityManager();

		if (entityManager.find(RegistryEntry.class, new RegistryEntry.RegistryEntryId("localhost", 8085, false, "microverse-rest-registry")) == null) {
			entityManager.getTransaction().begin();
			entityManager.persist(new RegistryEntry("localhost", 8085, false, "microverse-rest-registry"));
			entityManager.getTransaction().commit();
		}
	}


	@Test
	public void testManuallyCreatedEntityManager() {
		final CriteriaQuery<RegistryEntry> query = entityManager.getCriteriaBuilder().createQuery(RegistryEntry.class);
		final Root<RegistryEntry> root = query.from(RegistryEntry.class);
		final CriteriaQuery<RegistryEntry> all = query.select(root);
		final TypedQuery<RegistryEntry> allQuery = entityManager.createQuery(all);
		final List<RegistryEntry> registryEntries = allQuery.getResultList();

		Assertions.assertThat(registryEntries).isNotEmpty();
		System.out.println(registryEntries);
	}
}
