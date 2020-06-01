package add.entities;

public class PropertyPair {
    String key;
    String value;

    public PropertyPair(String a, String b) {
        super();
        this.key = a;
        this.value = b;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String a) {
        this.key = a;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String b) {
        this.value = b;
    }

}
