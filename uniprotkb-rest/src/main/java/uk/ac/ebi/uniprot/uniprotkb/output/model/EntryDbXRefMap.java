package uk.ac.ebi.uniprot.uniprotkb.output.model;

import com.google.common.base.Strings;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryDbXRefMap implements NamedValueMap {
    private static final String DR = "dr:";
    private final List<DbReference> dbReferences;
    private static final Map<String, String> D3MethodMAP = new HashMap<>();

    static {
        D3MethodMAP.put("X-ray", "X-ray crystallography");
        D3MethodMAP.put("NMR", "NMR spectroscopy");
        D3MethodMAP.put("EM", "Electron microscopy");
        D3MethodMAP.put("Model", "Model");
        D3MethodMAP.put("Neutron", "Neutron diffraction");
        D3MethodMAP.put("Fiber", "Fiber diffraction");
        D3MethodMAP.put("IR", "Infrared spectroscopy");
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(val -> val.startsWith(DR))
                || EntryGoXrefMap.contains(fields);

    }

    public EntryDbXRefMap(List<DbReference> dbReferences) {
        if (dbReferences == null) {
            this.dbReferences = Collections.emptyList();
        } else {
            this.dbReferences = Collections.unmodifiableList(dbReferences);
        }
    }

    @Override
    public Map<String, String> attributeValues() {
        if (dbReferences.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        Map<String, List<DbReference>> xrefMap = dbReferences.stream()
                .collect(Collectors.groupingBy(DbReference::getType));
        xrefMap.forEach((key, value) -> addToMap(map, key, value));
        return map;
    }

    private void addToMap(Map<String, String> map, String type, List<DbReference> xrefs) {
        DatabaseType dbType = DatabaseType.getDatabaseType(type);
        if (dbType == DatabaseType.GO) {
            EntryGoXrefMap dlGoXref = new EntryGoXrefMap(xrefs);
            Map<String, String> goMap = dlGoXref.attributeValues();
            goMap.entrySet().stream().forEach(val -> map.put(val.getKey(), val.getValue()));
        } else if (dbType == DatabaseType.PROTEOMES) {
            map.put(DR + dbType.name().toLowerCase(),
                    xrefs.stream().map(EntryDbXRefMap::proteomeXrefToString).collect(Collectors.joining("; ")));
        } else {
            map.put(DR + dbType.name().toLowerCase(),
                    xrefs.stream().map(EntryDbXRefMap::dbXrefToString).collect(Collectors.joining(";", "", ";")));
            if (dbType == DatabaseType.PDB) {
                map.put("3d", pdbXrefTo3DString(xrefs));
            }
        }
    }

    private String pdbXrefTo3DString(List<DbReference> xrefs) {
        Map<String, Long> result =
                xrefs.stream().flatMap(val -> val.getProperties().stream())
                        .filter(val -> val.getType().equalsIgnoreCase("method"))
                        .map(DbReference.Property::getValue)
                        .map(D3MethodMAP::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(val -> val, TreeMap::new, Collectors.counting()));

        return result.entrySet().stream()
                .map(val -> (val.getKey() + " (" + val.getValue().toString() + ")"))
                .collect(Collectors.joining("; "));
    }

    public static String dbXrefToString(DbReference xref) {
        StringBuilder sb = new StringBuilder();
        sb.append(xref.getId());
        if (!Strings.isNullOrEmpty(xref.getIsoform())) {
            sb.append(" [").append(xref.getIsoform()).append("]");
        }
        return sb.toString();
    }

    public static String proteomeXrefToString(DbReference xref) {
        StringBuilder sb = new StringBuilder();
        sb.append(xref.getId())
                .append(": ")
                .append(xref.getProperties().get(0).getValue());

        return sb.toString();
    }
}