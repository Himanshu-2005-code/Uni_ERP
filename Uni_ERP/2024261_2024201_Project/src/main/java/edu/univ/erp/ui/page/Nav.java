package edu.univ.erp.ui.page;

import java.util.function.Function;

/** A sidebar destination. */
public record Nav(String id, String label, String icon, String group, Function<Shell, Page> factory) {
}
