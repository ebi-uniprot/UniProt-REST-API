package org.uniprot.api.unisave.output.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;

import java.io.IOException;
import java.io.OutputStream;

/** @author eddturner */
public class UniSaveJSONMessageConverter extends JsonMessageConverter<UniSaveEntry> {

    public UniSaveJSONMessageConverter() {
        super(new ObjectMapper(), UniSaveEntry.class, null);
    }

    @Override
    protected void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = tlJsonGenerator.get();
        generator.writeObject(entity);
    }
}
