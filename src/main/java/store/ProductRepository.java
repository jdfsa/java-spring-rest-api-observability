package store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProductRepository extends JpaRepository<Product, Long> {
}
