package cz.muni.xmichalk.storage;

import cz.muni.fi.cpm.model.CpmDocument;

public class StorageCpmDocument {
    public CpmDocument document;
    public String jwt;

    public StorageCpmDocument(CpmDocument document, String jwt) {
        this.document = document;
        this.jwt = jwt;
    }
}
