package ec.edu.uteq.mercadoquevedo.catalogo.service;

import ec.edu.uteq.mercadoquevedo.catalogo.dto.ProductoRequest;
import ec.edu.uteq.mercadoquevedo.catalogo.exception.ProductoNoEncontradoException;
import ec.edu.uteq.mercadoquevedo.catalogo.exception.SkuDuplicadoException;
import ec.edu.uteq.mercadoquevedo.catalogo.model.Producto;
import ec.edu.uteq.mercadoquevedo.catalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Capa service: reglas de negocio (SKU único, existencia). No conoce HTTP ni SQL.
 */
@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Transactional
    public Producto crear(ProductoRequest datos) {
        repository.findBySku(datos.sku()).ifPresent(p -> {
            throw new SkuDuplicadoException(datos.sku());
        });

        Producto producto = new Producto();
        producto.setSku(datos.sku());
        producto.setNombre(datos.nombre());
        producto.setPrecio(datos.precio());
        producto.setStock(datos.stock());
        return repository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest datos) {
        Producto producto = obtener(id);

        repository.findBySku(datos.sku())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new SkuDuplicadoException(datos.sku());
                });

        producto.setSku(datos.sku());
        producto.setNombre(datos.nombre());
        producto.setPrecio(datos.precio());
        producto.setStock(datos.stock());
        return repository.save(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = obtener(id);
        repository.delete(producto);
    }
}
