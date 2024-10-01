package org.urbcomp.startdb.compress.elf.doubleprecision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListMapExample {
    public static void main(String[] args) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key1", new ArrayList<>());
        map.get("key1").add("value1");
        map.get("key1").add("value2");

        System.out.println(map.get("key1")); // Prints [value1, value2]
    }
}
