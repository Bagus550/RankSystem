package id.bagus.rank;

import java.util.List;

public record Rank(String id, String displayName, String prefix, int weight,
                  List<String> permissions, List<String> inherits, double playtimeHours, long exp) {
    /** True kalau rank ini punya syarat naik otomatis. */
    public boolean isAutoRank() {
        return playtimeHours > 0 || exp > 0;
    }
}
