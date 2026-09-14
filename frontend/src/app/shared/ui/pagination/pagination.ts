import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-pagination',
  styleUrl: './pagination.scss',
  templateUrl: './pagination.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  readonly currentPage = input(1);
  readonly totalPages = input(1);
  readonly disabled = input(false);

  readonly pageChange = output<number>();

  protected previous(): void {
    this.goTo(this.currentPage() - 1);
  }

  protected next(): void {
    this.goTo(this.currentPage() + 1);
  }

  protected goTo(page: number): void {
    if (this.disabled() || page < 1 || page > this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.pageChange.emit(page);
  }
}
