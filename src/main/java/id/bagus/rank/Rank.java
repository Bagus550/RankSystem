package id.bagus.rank;

import java.util.List;

public record Rank(String id, String displayName, String prefix, int weight,
                  List<String> permissions, List<String> inherits) {
}
