package com.project.util;

import com.project.dto.PaginationUtilDTO;
import com.project.exception.ValidationException;

public class PaginationUtil {

	private PaginationUtil() {
	}

	/**
	 * @param  pageNumber
	 * @param  pageSize
	 * @param  totalCount
	 * @return
	 * @throws ValidationException
	 */
	public static PaginationUtilDTO calculatePagination(final Integer pageNumber, final Integer pageSize,
			final long totalCount) throws ValidationException {
		if (pageNumber == null || pageSize == null || pageNumber == 0 || pageSize == 0) {
			throw new ValidationException("pageNumber or pageSize can not be null or Zero");
		}
		PaginationUtilDTO paginationUtilDto = new PaginationUtilDTO();
		boolean hasPreviousPage = false;
		boolean hasNextPage = false;
		long totalPages = totalCount / pageSize;
		if (totalCount % pageSize > 0) {
			totalPages += 1;
		}
		if (pageNumber != 1 && pageNumber <= totalPages) {
			hasPreviousPage = true;
		}
		if (pageNumber < totalPages) {
			hasNextPage = true;
		}

		paginationUtilDto.setStartIndex((pageNumber - 1) * pageSize);
		paginationUtilDto.setTotalPages(totalPages);
		paginationUtilDto.setPageNumber(pageNumber);
		paginationUtilDto.setHasPreviousPage(hasPreviousPage);
		paginationUtilDto.setHasNextPage(hasNextPage);
		return paginationUtilDto;

	}

}
