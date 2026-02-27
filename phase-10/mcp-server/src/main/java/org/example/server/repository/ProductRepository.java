package org.example.server.repository;

import org.example.server.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 商品数据访问层接口
 * 使用R2DBC响应式编程操作数据库
 */
@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    /**
     * 根据商品名称查询商品
     */
    Mono<Product> findByName(String name);

    /**
     * 根据分类查询商品列表
     */
    Flux<Product> findByCategory(String category);

    /**
     * 根据状态查询商品列表
     */
    Flux<Product> findByStatus(String status);

    /**
     * 根据价格范围查询商品
     */
    @Query("SELECT * FROM products WHERE price >= :minPrice AND price <= :maxPrice")
    Flux<Product> findByPriceRange(Double minPrice, Double maxPrice);

    /**
     * 模糊查询商品名称
     */
    @Query("SELECT * FROM products WHERE name LIKE '%' || :keyword || '%'")
    Flux<Product> findByNameContaining(String keyword);

    /**
     * 统计商品总数
     */
    @Query("SELECT COUNT(*) FROM products")
    Mono<Long> countAll();

    /**
     * 根据ID更新商品信息
     */
    @Query("UPDATE products SET name = :name, description = :description, price = :price, " +
            "stock = :stock, category = :category, status = :status, updated_at = NOW() WHERE id = :id")
    Mono<Integer> updateProduct(Long id, String name, String description, Double price, Integer stock, String category, String status);
}
