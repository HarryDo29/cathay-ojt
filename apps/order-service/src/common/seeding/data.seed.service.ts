import { Injectable, Logger, OnApplicationBootstrap } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Product } from '../../entities/product.entity.js';
import { Order } from '../../entities/order.entity.js';
import { OrderItem } from '../../entities/order-item.entity.js';
import { OrderStatus } from '../../entities/order.entity.js';

const SEED_USER_ID = '00000000-0000-0000-0000-000000000001';

@Injectable()
export class DataSeedService implements OnApplicationBootstrap {
  private readonly logger = new Logger('Seed order service data');

  constructor(
    @InjectRepository(Product)
    private readonly productRepository: Repository<Product>,
    @InjectRepository(Order)
    private readonly orderRepository: Repository<Order>,
    @InjectRepository(OrderItem)
    private readonly orderItemRepository: Repository<OrderItem>,
  ) {}

  async onApplicationBootstrap(): Promise<void> {
    const existingProducts = await this.productRepository.count();
    if (existingProducts > 0) {
      this.logger.log('Seed skipped because products already exist.');
      return;
    }

    await this.productRepository.insert([
      {
        name: 'Test Product A',
        description: 'Seed data for test environment',
        price: 100000,
        stockQuantity: 100,
        isActive: true,
      },
      {
        name: 'Test Product B',
        description: 'Seed data for test environment',
        price: 250000,
        stockQuantity: 50,
        isActive: true,
      },
      {
        name: 'Test Product C',
        description: 'Seed data for test environment',
        price: 500000,
        stockQuantity: 20,
        isActive: true,
      },
    ]);

    const orderCount = await this.orderRepository.count();
    if (orderCount > 0) {
      this.logger.log('Seed skipped because orders already exist.');
      return;
    }

    const products = await this.productRepository.find({
      order: { name: 'ASC' },
    });
    const [productA, productB, productC] = products;

    const orders = await this.orderRepository.save([
      {
        userId: SEED_USER_ID,
        totalAmount: 350000,
        status: OrderStatus.CONFIRMED,
      },
      {
        userId: SEED_USER_ID,
        totalAmount: 750000,
        status: OrderStatus.COMPLETED,
      },
      {
        userId: SEED_USER_ID,
        totalAmount: 100000,
        status: OrderStatus.PENDING,
      },
    ]);

    await this.orderItemRepository.save([
      {
        orderId: orders[0].id,
        productId: productA.id,
        quantity: 2,
        priceAtPurchase: 100000,
      },
      {
        orderId: orders[0].id,
        productId: productB.id,
        quantity: 1,
        priceAtPurchase: 250000,
      },
      {
        orderId: orders[1].id,
        productId: productB.id,
        quantity: 2,
        priceAtPurchase: 250000,
      },
      {
        orderId: orders[1].id,
        productId: productC.id,
        quantity: 1,
        priceAtPurchase: 500000,
      },
      {
        orderId: orders[2].id,
        productId: productA.id,
        quantity: 1,
        priceAtPurchase: 100000,
      },
    ]);

    this.logger.log('Seed completed (products, orders, order-items).');
  }
}
