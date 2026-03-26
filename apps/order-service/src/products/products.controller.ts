import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
  Request,
} from '@nestjs/common';
import type { Request as ExpressRequest } from 'express';
import { ProductsService } from './products.service.js';
import { CreateProductDto } from './dto/create-product.dto.js';
import { UpdateProductDto } from './dto/update-product.dto.js';

@Controller('products')
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  /**
   * Smoke test API Gateway (public, không cần JWT).
   * GET /api/v1/order-service/products/test
   */
  @Get('test')
  @HttpCode(HttpStatus.OK)
  async gatewayTest(@Request() req: ExpressRequest) {
    const ms = 2500;
    if (ms > 0) {
      await new Promise<void>((resolve) => setTimeout(resolve, ms));
    }
    return {
      ok: true,
      service: 'order-service',
      path: '/products/test',
      method: req.method,
      timestamp: new Date().toISOString(),
      fromGateway: {
        'x-user-id': req.headers['x-user-id'] ?? null,
        'x-user-email': req.headers['x-user-email'] ?? null,
        'x-user-role': req.headers['x-user-role'] ?? null,
        'x-internal-api-key': req.headers['x-internal-api-key']
          ? '(set)'
          : null,
        'public-endpoint': req.headers['public-endpoint'] ?? null,
      },
    };
  }

  @Post()
  async create(@Body() dto: CreateProductDto) {
    return this.productsService.create(dto);
  }

  @Get()
  async findAll() {
    return this.productsService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.productsService.findById(id);
  }

  @Patch(':id')
  async update(
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateProductDto,
  ) {
    return this.productsService.update(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async remove(@Param('id', ParseUUIDPipe) id: string) {
    await this.productsService.remove(id);
  }
}
