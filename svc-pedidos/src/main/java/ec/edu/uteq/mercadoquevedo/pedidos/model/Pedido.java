package ec.edu.uteq.mercadoquevedo.pedidos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio Pedido — tabla propia de svc-pedidos (Database per Service).
 * NO comparte base de datos con svc-catalogo: solo guarda una referencia
 * (productoId) al recurso remoto, nunca una copia de su estado interno.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String estado = "CREADO"; // CREADO | RECHAZADO

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> items = new ArrayList<>();

    @PrePersist
    protected void alCrear() {
        this.creadoEn = OffsetDateTime.now();
    }

    /** Mantiene ambos lados de la relación bidireccional sincronizados. */
    public void agregarItem(PedidoItem item) {
        items.add(item);
        item.setPedido(this);
    }
}
