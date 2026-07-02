package ec.edu.uteq.mercadoquevedo.pedidos.repository;

import ec.edu.uteq.mercadoquevedo.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
