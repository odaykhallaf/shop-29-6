package com.khomsi.site_project.service;

import com.khomsi.site_project.entity.Product;
import com.khomsi.site_project.exception.ProductNotFoundException;
import com.khomsi.site_project.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class ProductService implements IProductService {
    @Autowired
    private ProductRepository productRepository;

    public static final int PRODUCTS_PER_PAGE = 10;
    public static final int SEARCH_RESULTS_PAGE = 10;
    public static final int PRODUCTS_PER_ADMIN_PAGE = 5;

    @Override
    public Page<Product> listByCategory(int pageNum, Integer categoryId) {
        String categoryIdMatch = "-" + String.valueOf(categoryId) + "-";
        Pageable pageable = PageRequest.of(pageNum - 1, PRODUCTS_PER_PAGE);
        Page<Product> page = productRepository.listByCategory(categoryId, pageable, categoryIdMatch);
        page.forEach(product -> product.setFormattedPrice(formatPrice(product.getPrice())));
        return page;
    }

    @Override
    public Page<Product> listByPage(int pageNum, String sortField, String sortDir, String keyword,
                                    Integer categoryId) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, PRODUCTS_PER_ADMIN_PAGE, sort);
        Page<Product> page;

        if (keyword != null && !keyword.isEmpty()) {
            if (categoryId != null && categoryId > 0) {
                String categoryIdMatch = "-" + String.valueOf(categoryId) + "-";
                page = productRepository.searchInCategory(categoryId, categoryIdMatch, keyword, pageable);
            } else {
                page = productRepository.findAll(keyword, pageable);
            }
        } else {
            if (categoryId != null && categoryId > 0) {
                String categoryIdMatch = "-" + String.valueOf(categoryId) + "-";
                page = productRepository.findAllInCategory(categoryId, categoryIdMatch, pageable);
            } else {
                page = productRepository.findAll(pageable);
            }
        }

        page.forEach(product -> product.setFormattedPrice(formatPrice(product.getPrice())));
        return page;
    }

    @Override
    public List<Product> getAllProducts() throws ProductNotFoundException {
        List<Product> listProducts = productRepository.findAll();
        if (listProducts.isEmpty()) {
            throw new ProductNotFoundException("Couldn't find any product in DB");
        }
        listProducts.forEach(product -> product.setFormattedPrice(formatPrice(product.getPrice())));
        return listProducts;
    }

    @Override
    public List<Product> getRandomAmountOfProducts() throws ProductNotFoundException {
        List<Product> productList = productRepository.findAllByCategoryId(4);
        if (productList.isEmpty()) {
            throw new ProductNotFoundException("Couldn't find any product in DB");
        }
        Collections.shuffle(productList);
        int randomSeriesLength = 1;
        productList.forEach(product -> product.setFormattedPrice(formatPrice(product.getPrice())));
        return productList.subList(0, randomSeriesLength);
    }

    @Override
    public void saveProduct(Product product) {
        if (product.getAlias() == null || product.getAlias().isEmpty()) {
            String defaultAlias = product.getTitle().toLowerCase();
            product.setAlias((new CategoryService().convertCyrillic(defaultAlias).replaceAll(" ", "_")));
        } else {
            product.setAlias(product.getAlias().replaceAll(" ", "_").toLowerCase());
        }
        product.setFormattedPrice(formatPrice(product.getPrice()));
        productRepository.save(product);
    }

    @Override
    public Product getProduct(Integer id) throws ProductNotFoundException {
        try {
            Product product = productRepository.getReferenceById(id);
            product.setFormattedPrice(formatPrice(product.getPrice()));
            return product;
        } catch (NoSuchElementException e) {
            throw new ProductNotFoundException("Couldn't find any product with id " + id);
        }
    }

    @Override
    public Product getProduct(String alias) throws ProductNotFoundException {
        try {
            Product product = productRepository.findByAlias(alias);
            product.setFormattedPrice(formatPrice(product.getPrice()));
            return product;
        } catch (NoSuchElementException e) {
            throw new ProductNotFoundException("Couldn't find any product with alias " + alias);
        }
    }

    @Override
    public void deleteProduct(Integer id) throws ProductNotFoundException {
        Long countById = productRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new ProductNotFoundException("Couldn't find any product with ID " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public String checkUnique(Integer id, String title) {
        boolean isCreatingNew = (id == null || id == 0);
        Product productByName = productRepository.findByTitle(title);
        if (isCreatingNew) {
            if (productByName != null) return "Duplicate";
        } else {
            if (productByName != null && !Objects.equals(productByName.getId(), id)) {
                return "Duplicate";
            }
        }
        return "OK";
    }

    public Page<Product> search(String keyword, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum - 1, SEARCH_RESULTS_PAGE);
        Page<Product> page = productRepository.search(keyword, pageable);
        page.forEach(product -> product.setFormattedPrice(formatPrice(product.getPrice())));
        return page;
    }

    public String formatPrice(int price) {
        return price + " ₪";
    }
}
