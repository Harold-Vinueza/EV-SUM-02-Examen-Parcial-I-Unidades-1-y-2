package ec.edu.uteq.mercadoquevedo.pedidos.service;

import ec.edu.uteq.mercadoquevedo.pedidos.client.CatalogoClient;
import ec.edu.uteq.mercadoquevedo.pedidos.client.ProductoDTO;
import ec.edu.uteq.mercadoquevedo.pedidos.dto.ItemPedidoRequest;
import ec.edu.uteq.mercadoquevedo.pedidos.dto.PedidoRequest;
import ec.edu.uteq.mercadoquevedo.pedidos.exception.PedidoNoEncontradoException;
import ec.edu.uteq.mercadoquevedo.pedidos.exception.StockInsuficienteException;
import ec.edu.uteq.mercadoquevedo.pedidos.model.Pedido;
import ec.edu.uteq.mercadoquevedo.pedidos.model.PedidoItem;
import ec.edu.uteq.mercadoquevedo.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Capa service: orquesta la creación de un pedido validando, por cada item,
 * existencia y stock contra svc-catalogo (comunicación síncrona REST).
 * Nota: las excepciones ProductoInexistenteException y
 * CatalogoNoDisponibleException que lanza CatalogoClient se propagan tal
 * cual hasta el GlobalExceptionHandler — no necesitan traducción adicional
 * porque ya son excepciones de dominio de svc-pedidos.
 */
@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final CatalogoClient catalogoClient;

    public PedidoService(PedidoRepository repository, CatalogoClient catalogoClient) {
        this.repository = repository;
        this.catalogoClient = catalogoClient;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    @Transactional
    public Pedido crear(PedidoRequest datos) {
        Pedido pedido = new Pedido();
        BigDecimal subtotalTotal = BigDecimal.ZERO;

        for (ItemPedidoRequest itemReq : datos.items()) {
            // Puede lanzar ProductoInexistenteException o CatalogoNoDisponibleException
            ProductoDTO producto = catalogoClient.obtenerProducto(itemReq.productoId());

            if (producto.stock() < itemReq.cantidad()) {
                throw new StockInsuficienteException(
                        itemReq.productoId(), producto.stock(), itemReq.cantidad());
            }

            BigDecimal subtotalItem = producto.precio().multiply(BigDecimal.valueOf(itemReq.cantidad()));

            PedidoItem item = new PedidoItem();
            item.setProductoId(itemReq.productoId());
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(producto.precio());
            item.setSubtotalItem(subtotalItem);

            pedido.agregarItem(item);
            subtotalTotal = subtotalTotal.add(subtotalItem);
        }

        pedido.setSubtotal(subtotalTotal);
        return repository.save(pedido);
    }
}
