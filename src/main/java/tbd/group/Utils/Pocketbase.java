package tbd.group.Utils;

import java.util.List;

public class Pocketbase {
    private static final String POCKETBASE_URL = "127.0.0.1"; //TODO: update

    public static List<String> getRecords(String collectionIdentifier) {
        return null;
    }

    public static List<String> getRecords(String collectionIdentifier, RecordQuery query) {
        return null;
    }

    static class RecordQuery {
        private int page;
        private int perPage;
        private String sort;
        private String filter;
        private String expand;
        private String fields;
        private boolean skipTotal;
    }
}
