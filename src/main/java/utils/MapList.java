package utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Matias Martinez
 */
public class MapList<K, V> extends LinkedHashMap<K, List<V>> {

    public void add(K key, V value) {
        List<V> listV;
        if (!this.containsKey(key)) {
            listV = new ArrayList<V>();
            this.put(key, listV);
        } else {
            listV = get(key);
        }
        listV.add(value);
    }

    public MapList<K, V> getSorted() {
        return this.entrySet().stream()
                .sorted(Map.Entry.<K, List<V>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, MapList::new));

    }
}
