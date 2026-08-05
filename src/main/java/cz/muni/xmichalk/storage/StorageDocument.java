package cz.muni.xmichalk.storage;

import org.openprovenance.prov.model.Document;

public class StorageDocument {
    public Document document;
    public String jwt;

    public StorageDocument(Document document, String jwt) {
        this.document = document;
        this.jwt = jwt;
    }
}
