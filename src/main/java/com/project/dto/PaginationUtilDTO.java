package com.project.dto;

import lombok.Data;

@Data
public class PaginationUtilDTO {

	private Integer startIndex;
	private Integer pageNumber;
	private Long totalPages;
	private Boolean hasPreviousPage;
	private Boolean hasNextPage;
	private Object data;

}
