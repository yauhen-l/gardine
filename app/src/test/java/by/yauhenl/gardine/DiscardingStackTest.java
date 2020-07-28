package by.yauhenl.gardine;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.*;

public class DiscardingStackTest {

    private static final String a = "a", b = "b", c = "c", d = "d";

    @Test
    public void singleElement_noTruncation() {
        DiscardingStack<String> ra = new DiscardingStack<>(3);
        ra.add(a);
        ra.add(a);
        ra.add(a);
        assertThat(ra.getAll()).containsOnly(a);
    }

    @Test
    public void twoElements_noTruncation() {
        DiscardingStack<String> ra = new DiscardingStack<>(3);
        ra.add(a);
        ra.add(b);
        ra.add(a);
        assertThat(ra.getAll()).containsOnly(a, b);
    }

    @Test
    public void threeElements_withTruncation() {
        DiscardingStack<String> ra = new DiscardingStack<>(3);
        ra.add(a);
        ra.add(b);
        ra.add(c);
        ra.add(d);
        assertThat(ra.getAll()).containsOnly(b, c, d);
    }

    
}
