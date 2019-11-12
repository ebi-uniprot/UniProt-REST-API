package org.uniprot.api.common.repository.store;

import lombok.Data;

/**
 * This class represents configurable properties of {@link StoreStreamer} instances.
 *
 * <p>Created 22/08/18
 *
 * @author Edd
 */
@Data
public class StreamerConfigProperties {
    private int storeBatchSize;
    private int storeFetchMaxRetries;
    private int storeFetchRetryDelayMillis;
}
