package ec.edu.uteq.mercadoquevedo.catalogo.repository;

import ec.edu.uteq.mercadoquevedo.catalogo.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Capa repository: única capa que conoce Spring Data JPA / SQL.
 * El service no sabe cómo se persisten los datos, solo pide/entrega entidades.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);
}
