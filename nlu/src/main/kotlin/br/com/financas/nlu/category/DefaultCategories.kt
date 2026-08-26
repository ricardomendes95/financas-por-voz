package br.com.financas.nlu.category

import br.com.financas.nlu.model.CategoryRef
import br.com.financas.nlu.model.CategoryRule
import br.com.financas.nlu.model.TransactionType

/**
 * Seed inicial de categorias e regras.
 *
 * O peso resolve sobreposições reais: "assinatura" existe tanto em
 * Entretenimento quanto em Serviços, e o usuário quase sempre quer o primeiro.
 * Regras criadas pelo usuário nascem com peso 100 e vencem qualquer uma daqui.
 */
object DefaultCategories {

    object Id {
        const val FOOD = "cat_alimentacao"
        const val TRANSPORT = "cat_transporte"
        const val HOUSING = "cat_moradia"
        const val HEALTH = "cat_saude"
        const val ENTERTAINMENT = "cat_entretenimento"
        const val EDUCATION = "cat_educacao"
        const val SHOPPING = "cat_compras"
        const val SERVICES = "cat_servicos"
        const val PETS = "cat_pets"
        const val TAXES = "cat_impostos"
        const val OTHER_EXPENSE = "cat_outros"

        const val SALARY = "cat_salario"
        const val FREELANCE = "cat_freelance"
        const val REFUND = "cat_reembolso"
        const val OTHER_INCOME = "cat_outras_receitas"
    }

    val categories: List<CategoryRef> = listOf(
        CategoryRef(Id.FOOD, "Alimentação", TransactionType.EXPENSE),
        CategoryRef(Id.TRANSPORT, "Transporte", TransactionType.EXPENSE),
        CategoryRef(Id.HOUSING, "Moradia", TransactionType.EXPENSE),
        CategoryRef(Id.HEALTH, "Saúde", TransactionType.EXPENSE),
        CategoryRef(Id.ENTERTAINMENT, "Entretenimento", TransactionType.EXPENSE),
        CategoryRef(Id.EDUCATION, "Educação", TransactionType.EXPENSE),
        CategoryRef(Id.SHOPPING, "Compras", TransactionType.EXPENSE),
        CategoryRef(Id.SERVICES, "Serviços", TransactionType.EXPENSE),
        CategoryRef(Id.PETS, "Pets", TransactionType.EXPENSE),
        CategoryRef(Id.TAXES, "Impostos e Taxas", TransactionType.EXPENSE),
        CategoryRef(Id.OTHER_EXPENSE, "Outros", TransactionType.EXPENSE),
        CategoryRef(Id.SALARY, "Salário", TransactionType.INCOME),
        CategoryRef(Id.FREELANCE, "Freelance", TransactionType.INCOME),
        CategoryRef(Id.REFUND, "Reembolso", TransactionType.INCOME),
        CategoryRef(Id.OTHER_INCOME, "Outras Receitas", TransactionType.INCOME)
    )

    val rules: List<CategoryRule> = buildList {
        addAll(Id.FOOD to 20 keywords listOf(
            "pastel", "lanche", "almoco", "janta", "jantar", "cafe", "mercado",
            "supermercado", "padaria", "acai", "pizza", "hamburguer", "burger",
            "ifood", "rappi", "comida", "marmita", "feira", "hortifruti",
            "restaurante", "sorvete", "salgado", "coxinha", "tapioca", "sushi",
            "churrasco", "bolo", "doce", "chocolate", "refrigerante", "suco",
            "atacadao", "assai", "carrefour", "big", "hiper", "quitanda",
            "acougue", "peixaria", "delivery", "cantina", "lanchonete"
        ))

        addAll(Id.TRANSPORT to 20 keywords listOf(
            "uber", "99", "taxi", "gasolina", "combustivel", "alcool", "etanol",
            "diesel", "onibus", "passagem", "metro", "estacionamento", "pedagio",
            "mecanico", "oleo", "pneu", "lavagem", "ipva", "seguro do carro",
            "moto", "bicicleta", "patinete", "posto", "revisao", "uber eats",
            "carro", "conserto"
        ))

        addAll(Id.HOUSING to 20 keywords listOf(
            "aluguel", "condominio", "energia", "luz", "agua", "gas", "iptu",
            "internet", "faxina", "diarista", "reforma", "movel", "moveis",
            "eletrodomestico", "celpe", "neoenergia", "compesa", "wifi",
            "manutencao", "encanador", "eletricista", "material de construcao"
        ))

        addAll(Id.HEALTH to 20 keywords listOf(
            "farmacia", "remedio", "medicamento", "consulta", "exame", "dentista",
            "medico", "plano de saude", "academia", "psicologo", "terapia",
            "fisioterapia", "vacina", "laboratorio", "hospital", "clinica",
            "oculos", "lente", "suplemento", "unimed", "hapvida", "drogaria",
            "drogasil", "pacheco", "pague menos"
        ))

        addAll(Id.ENTERTAINMENT to 30 keywords listOf(
            "assinatura", "netflix", "spotify", "cinema", "jogo", "game",
            "steam", "disney", "prime video", "hbo", "max", "youtube premium",
            "bar", "cerveja", "show", "festa", "balada", "viagem", "passeio",
            "streaming", "playstation", "xbox", "nintendo", "livro de ficcao",
            "crunchyroll", "deezer", "twitch", "ingresso", "teatro", "parque"
        ))

        addAll(Id.EDUCATION to 20 keywords listOf(
            "curso", "livro", "faculdade", "mensalidade escolar", "escola",
            "udemy", "alura", "certificacao", "material escolar", "apostila",
            "workshop", "palestra", "treinamento", "ingles", "idioma"
        ))

        addAll(Id.SHOPPING to 20 keywords listOf(
            "roupa", "camisa", "calca", "tenis", "sapato", "celular",
            "eletronico", "shopee", "mercado livre", "amazon", "aliexpress",
            "presente", "perfume", "cosmetico", "maquiagem", "bolsa", "relogio",
            "fone", "notebook", "monitor", "teclado", "mouse", "magalu",
            "americanas", "shein", "temu", "renner", "riachuelo"
        ))

        addAll(Id.SERVICES to 15 keywords listOf(
            "mensalidade", "seguro", "tarifa", "anuidade", "cartorio",
            "contador", "advogado", "barbeiro", "cabeleireiro", "salao",
            "manicure", "lavanderia", "correios", "frete", "banco"
        ))

        addAll(Id.PETS to 20 keywords listOf(
            "racao", "veterinario", "petshop", "pet shop", "banho e tosa",
            "antipulgas", "vermifugo", "areia de gato", "brinquedo de pet"
        ))

        addAll(Id.TAXES to 20 keywords listOf(
            "imposto", "das", "inss", "multa", "taxa", "darf", "irpf",
            "licenciamento", "juros", "mora"
        ))

        addAll(Id.SALARY to 30 keywords listOf(
            "salario", "holerite", "contracheque", "pagamento do mes",
            "decimo terceiro", "ferias", "adiantamento"
        ))

        addAll(Id.FREELANCE to 30 keywords listOf(
            "freela", "freelance", "projeto", "bico", "pj", "consultoria",
            "servico prestado", "nota fiscal"
        ))

        addAll(Id.REFUND to 30 keywords listOf(
            "reembolso", "estorno", "devolucao", "cashback", "restituicao"
        ))

        addAll(Id.OTHER_INCOME to 15 keywords listOf(
            "presente recebido", "rendimento", "dividendo", "aluguel recebido",
            "venda", "vendi"
        ))
    }

    private infix fun Pair<String, Int>.keywords(list: List<String>): List<CategoryRule> =
        list.map { CategoryRule(keyword = it, categoryId = first, weight = second) }
}
