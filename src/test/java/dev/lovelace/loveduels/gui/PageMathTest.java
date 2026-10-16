package dev.lovelace.loveduels.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageMathTest {

    @Test
    void pageCountRoundsUpAndIsAtLeastOne() {
        assertEquals(1, PageMath.pageCount(0, 21));
        assertEquals(1, PageMath.pageCount(21, 21));
        assertEquals(2, PageMath.pageCount(22, 21));
        assertEquals(3, PageMath.pageCount(43, 21));
    }

    @Test
    void clampHandlesShrinkingList() {
        assertEquals(0, PageMath.clampPage(-3, 50, 21));
        assertEquals(2, PageMath.clampPage(9, 50, 21));
        assertEquals(0, PageMath.clampPage(2, 5, 21));
        assertEquals(0, PageMath.clampPage(1, 0, 21));
    }

    @Test
    void indexRange() {
        assertEquals(21, PageMath.fromIndex(1, 21));
        assertEquals(42, PageMath.toIndex(1, 50, 21));
        assertEquals(50, PageMath.toIndex(2, 50, 21));
    }
}
