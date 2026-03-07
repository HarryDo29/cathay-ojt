import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, In, Repository } from 'typeorm';
import { Order, OrderStatus } from '../entities/order.entity.js';
import { OrderItem } from '../entities/order-item.entity.js';
import { Product } from '../entities/product.entity.js';
import { CreateOrderDto } from './dto/create-order.dto.js';
import { UpdateOrderStatusDto } from './dto/update-order-status.dto.js';

@Injectable()
export class OrdersService {
  constructor(
    @InjectRepository(Order)
    private readonly orderRepo: Repository<Order>,
    @InjectRepository(OrderItem)
    private readonly orderItemRepo: Repository<OrderItem>,
    private readonly dataSource: DataSource,
  ) {}

  async create(userId: string, dto: CreateOrderDto): Promise<Order | null> {
    return this.dataSource.transaction(async (manager) => {
      const productIds = dto.orderItems.map((i) => i.productId);
      const products = await manager.find(Product, {
        where: { id: In(productIds) },
        lock: { mode: 'pessimistic_write' },
      });

      const productMap = new Map(products.map((p) => [p.id, p]));

      let totalAmount = 0;
      const orderItems: Partial<OrderItem>[] = [];
      const newOrder = manager.create(Order, {
        userId,
        totalAmount: 0,
        status: OrderStatus.PENDING,
      });
      const savedOrder = await manager.save(Order, newOrder);

      for (const item of dto.orderItems) {
        const product = productMap.get(item.productId);

        if (!product) {
          throw new NotFoundException(`Product ${item.productId} not found`);
        }
        if (!product.isActive) {
          throw new BadRequestException(
            `Product "${product.name}" is not available`,
          );
        }
        if (product.stockQuantity < item.quantity) {
          throw new BadRequestException(
            `Insufficient stock for "${product.name}". Available: ${product.stockQuantity}`,
          );
        }

        product.stockQuantity -= item.quantity;
        const itemTotal = Number(product.price) * item.quantity;
        totalAmount += itemTotal;
        const orderItem = manager.create(OrderItem, {
          orderId: savedOrder.id,
          productId: product.id,
          quantity: item.quantity,
          priceAtPurchase: Number(product.price),
        });
        orderItems.push(orderItem);
      }

      await manager.save(OrderItem, orderItems);
      await manager.update(Order, savedOrder.id, {
        totalAmount: Math.round(totalAmount * 100) / 100,
        // items: orderItems,
      });
      return manager.findOne(Order, {
        where: { id: savedOrder.id },
      });
    });
  }

  async findAll(): Promise<Order[]> {
    return this.orderRepo.find({
      relations: { items: { product: true } },
      order: { createdAt: 'DESC' },
    });
  }

  async findById(id: string): Promise<Order> {
    const order = await this.orderRepo.findOne({
      where: { id },
      // relations: { items: { product: true } },
    });

    if (!order) {
      throw new NotFoundException(`Order ${id} not found`);
    }
    return order;
  }

  async updateStatus(id: string, dto: UpdateOrderStatusDto): Promise<Order> {
    const order = await this.findById(id);

    if (order.status === OrderStatus.CANCELLED) {
      throw new BadRequestException('Cannot update a cancelled order');
    }
    if (order.status === OrderStatus.COMPLETED) {
      throw new BadRequestException('Cannot update a completed order');
    }

    order.status = dto.status;
    return this.orderRepo.save(order);
  }

  async cancel(id: string): Promise<Order> {
    return this.dataSource.transaction(async (manager) => {
      const order = await manager.findOne(Order, {
        where: { id },
        relations: { items: true },
      });

      if (!order) {
        throw new NotFoundException(`Order ${id} not found`);
      }
      if (order.status === OrderStatus.CANCELLED) {
        throw new BadRequestException('Order is already cancelled');
      }
      if (order.status === OrderStatus.COMPLETED) {
        throw new BadRequestException('Cannot cancel a completed order');
      }

      const productIds = order.items.map((i) => i.productId);
      const products = await manager.find(Product, {
        where: { id: In(productIds) },
        lock: { mode: 'pessimistic_write' },
      });
      const productMap = new Map(products.map((p) => [p.id, p]));

      for (const item of order.items) {
        const product = productMap.get(item.productId);
        if (product) {
          product.stockQuantity += item.quantity;
        }
      }

      await manager.save(Product, products);

      order.status = OrderStatus.CANCELLED;
      return manager.save(Order, order);
    });
  }
}
