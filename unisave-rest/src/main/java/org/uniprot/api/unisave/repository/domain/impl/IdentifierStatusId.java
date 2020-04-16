package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;

import java.io.Serializable;

@Data
public class IdentifierStatusId implements Serializable {

    private EventTypeEnum eventType;

    private String sourceAccession;

    private String targetAccession;
}
