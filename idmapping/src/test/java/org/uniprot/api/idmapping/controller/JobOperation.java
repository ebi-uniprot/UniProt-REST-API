package org.uniprot.api.idmapping.controller;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public interface JobOperation {
    IdMappingJob createAndPutJobInCache() throws Exception;

    IdMappingJob createAndPutJobInCache(String from, String to, String fromIds)
            throws InvalidKeySpecException, NoSuchAlgorithmException;
}
