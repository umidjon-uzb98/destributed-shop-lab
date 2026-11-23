package uz.demo.dockerlab.product;

import java.util.List;

public record ProductListView(
        List<ProductView> items
) {}